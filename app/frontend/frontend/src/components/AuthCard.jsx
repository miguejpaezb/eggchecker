import PropTypes from 'prop-types';

import useAnimatedHeight from '../hooks/useAnimatedHeight';

function AuthCard({ children }) {
  const { contentRef, height } = useAnimatedHeight();

  return (
    <div
      className="ec-auth__card ec-auth__card--animated"
      style={height === null ? undefined : { height }}
    >
      <div ref={contentRef} className="ec-auth__card-inner">
        {children}
      </div>
    </div>
  );
}

AuthCard.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AuthCard;
